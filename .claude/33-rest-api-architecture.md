---
title: REST API Architecture
slug: rest-api-architecture
status: published
heading_type: heading
---

**I build the same nine files for every entity, in the same order, every time.** Not because creativity is wasted on CRUD — because it isn't. The interesting engineering in a backend is the eligibility matcher, the pricing rule, the state machine. The plumbing that gets a domain object safely into a database and back out over HTTP should be boring, consistent, and fast to produce, so the time and judgment go where they're worth something.

This is the layout, worked through against a real entity — `Company` — from a production Spring Boot codebase.

## The nine layers

One Java domain object in, nine files out, always in this order:

1. **Domain** — a plain POJO, no framework annotations, no persistence concerns
2. **Request DTOs** — one for create, one for update, each with its own validation rules
3. **Response DTO** — only what the client should see
4. **Entity** — the JPA-mapped shape of the same object
5. **Mapper** — domain ↔ entity, nothing else
6. **Repository** — Spring Data, plus the finders this entity actually needs
7. **DB service** — CRUD against the repository, one failure-handling pattern
8. **Business service** — validation, transactions, business rules
9. **Controller + converter** — HTTP in, DTOs out, wired to the business service

Every layer has exactly one job. A bug in validation is a request-DTO problem. A bug in a query is a repository problem. Nothing is guessing at two responsibilities at once.

## Domain: the thing itself

```java
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Company extends BaseDomain {
    private String code;
    private String name;
    private String description;
}
```

`BaseDomain` carries `id`, `extid`, `createdAt`, `updatedAt`, `deletedAt`, and `active` — audit and identity fields every entity needs, written once. It's `@Data @SuperBuilder @NoArgsConstructor` and nothing more; the equals/hashCode contract (`@EqualsAndHashCode(callSuper = true)`) and the all-args constructor are opted into by each subclass, not inherited automatically. This class only states what makes a `Company` a `Company`. No JPA, no HTTP, no validation. It's the one class every other layer converts to or from, and the only one a reviewer needs to read to know what the entity *means*.

## Request and response: never the same shape

```java
@EqualsAndHashCode(callSuper = true)
@Data
public class RequestCompanyCreate extends BaseRequest {

    @NotEmpty(message = "The code is required.")
    @Size(max = 8, message = "The code must be at most 8 characters.")
    private String code;

    @NotEmpty(message = "The name is required.")
    @Size(max = 64, message = "The name must be at most 64 characters.")
    private String name;

    @NotEmpty(message = "The description is required.")
    @Size(max = 128, message = "The description must be at most 128 characters.")
    private String description;
}
```

Create and update are different DTOs on purpose — update makes every field optional (no `@NotEmpty`), because a `PUT`/`PATCH` shouldn't have to resend the whole object to change one field:

```java
@EqualsAndHashCode(callSuper = true)
@Data
public class RequestCompanyUpdate extends BaseRequest {

    @Size(max = 8, message = "The code must be at most 8 characters.")
    private String code;

    @Size(max = 64, message = "The name must be at most 64 characters.")
    private String name;

    @Size(max = 128, message = "The description must be at most 128 characters.")
    private String description;
}
```

The response DTO is narrower still:

```java
@Data
@Builder
public class ResponseCompany {
    private String extid;
    private String code;
    private String name;
    private String description;
}
```

No internal `id`. Not because someone remembered to strip it — because the response type never had a field to leak in the first place. That's a design decision expressed as a compiler guarantee instead of a code-review checklist item.

## Entity: the database's opinion of the object

```java
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "company")
public class CompanyDb extends BaseDb {

    private static final long serialVersionUID = 330515747211210728L;

    @Column(name = "code", length = 16, unique = true)
    private String code;

    @Column(name = "name", length = 32, nullable = false, unique = true)
    private String name;

    @Column(name = "description", length = 255, nullable = false)
    private String description;
}
```

Same fields as `Company`, deliberately duplicated rather than shared, because a domain object and a persistence mapping have different reasons to change. The unique constraint on `name` (and, when supplied, `code`) lives here and in the matching Liquibase changeset — not in the domain layer, which shouldn't know that a database enforces it. Note the column lengths aren't a copy of the request DTO's `@Size` limits — the DTO can be stricter than the schema; the schema is the hard floor.

A mapper is the only thing allowed to convert between the two:

```java
@Component
@NoArgsConstructor
public class CompanyMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Company toModel(CompanyDb item) {
        return modelMapper.map(item, Company.class);
    }

    public CompanyDb toDb(Company item) {
        return modelMapper.map(item, CompanyDb.class);
    }

    public List<Company> toModelList(List<CompanyDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<CompanyDb> toDbList(List<Company> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
```

The null check on the list methods is there because an earlier version of this pattern returned `null` for a `null` input, and every caller had to remember to guard against it. Now the guarantee is in one place: this always returns a list.

## DB service: one failure pattern, applied every time

```java
@Slf4j
@Service
public class CompanyDbService extends BaseDbService {

    private final CompanyRepository repository;
    private final CompanyMapper mapper;

    public CompanyDbService(CompanyRepository repository, CompanyMapper mapper) {
        super("CompanyDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Company create(String code, @NonNull String name, @NonNull String description) {
        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            CompanyDb record = new CompanyDb();
            record.setExtid(extid);

            // Auto-generate code if not provided
            if (code == null || code.trim().isEmpty()) {
                String generatedCode = CodeGenerator.generateCode(
                    name,
                    c -> repository.findByCode(c).isPresent()
                );
                record.setCode(generatedCode);
            } else {
                record.setCode(code);
            }

            record.setName(name);
            record.setDescription(description);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            CompanyDb saved = repository.save(record);
            log.info(createdMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            log.error(failedOperationMessage("create", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("create"), e);
        }
    }
    // update, delete, findByExtid, findAll(), findAll(pageable), findByActive(...)
}
```

`BaseDbService` itself carries no generic CRUD — it only standardizes the log/exception *messages* (`createdMessage`, `updatedMessage`, `notFoundMessage`, `failedOperationMessage`, …) so every entity's logs read the same way. Each DB service still writes its own try/catch, but the shape never varies: generate the `extid` (a UUID never exposed as the database's auto-increment `id`), set audit timestamps, save, log, and on failure wrap in `DatabaseFailureException` with the same formatted message. Delete is soft: `setDeletedAt` plus `ActiveEnum.INACTIVE`, never a SQL `DELETE`. Nothing here is entity-specific except the field names and the occasional business quirk (`Company` auto-generates its `code` when one isn't supplied) — a reviewer who's read one `DbService` has read all of them.

## Business service: where the actual rules live

```java
@Slf4j
@Service
@Transactional(readOnly = true)
public class CompanyService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "code", "createdAt", "updatedAt");

    private final CompanyDbService dbService;

    public CompanyService(CompanyDbService dbService) {
        super(Company.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Company create(Company item) {
        requireNonNull(item, "Company");
        try {
            return dbService.create(item.getCode(), item.getName(), item.getDescription());
        } catch (DatabaseFailureException e) {
            throw new ServiceException("Unable to create company", e);
        }
    }
    // update, delete, findByExtid, findAll(), findAll(pageable, active) with a page-size cap
    // and a sort-field whitelist, findByActive...
}
```

`BaseService` itself is just a name and two guard helpers, `requireNonNull`/`requireNonBlank` — the `@Service`/`@Transactional` annotations and everything else live on the subclass. This is the layer that's allowed to know business rules — a page-size cap so a client can't request 100,000 rows, a sort-field whitelist so `ORDER BY` can't be pointed at an arbitrary column, `ResourceNotFoundException` thrown when the DB service reports nothing found. `DatabaseFailureException` bubbling out of the DB service is caught here and re-thrown as `ServiceException`, so the controller layer only ever deals with service-level exceptions.

## Controller: HTTP, and nothing else

```java
@RestController
@RequestMapping("/api/company")
@Validated
@Tag(name = "Company", description = "Company CRUD endpoints")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyConverter converter = new CompanyConverter();

    @PostMapping
    public ResponseEntity<ResponseCompany> create(@Valid @RequestBody RequestCompanyCreate request) {
        Company created = companyService.create(converter.toDomain(request));
        URI location = URI.create("/api/company/" + created.getExtid());
        return ResponseEntity.created(location).body(converter.toResponse(created));
    }

    @PutMapping("/{extid}")
    public ResponseCompany update(@PathVariable String extid, @Valid @RequestBody RequestCompanyUpdate request) {
        converter.validateUpdateRequest(request);
        return converter.toResponse(companyService.update(extid, converter.toDomain(request)));
    }
    // GET (paged list + single), PATCH (delegates to update), DELETE (soft, 204/404)
}
```

```java
class CompanyConverter {

    Company toDomain(RequestCompanyCreate request) {
        return Company.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    Company toDomain(RequestCompanyUpdate request) {
        return Company.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    ResponseCompany toResponse(Company item) {
        return ResponseCompany.builder()
                .extid(item.getExtid())
                .code(item.getCode())
                .name(item.getName())
                .description(item.getDescription())
                .build();
    }

    List<ResponseCompany> toResponse(List<Company> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestCompanyUpdate request) {
        if (request.getCode() == null &&
                request.getName() == null &&
                request.getDescription() == null) {
            throw new ValidationException("At least one field must be provided for update.");
        }
    }
}
```

The converter is a package-private class living in the *same file* as the controller — but it's a **top-level** class, not a nested inner class, and that's deliberate rather than a style preference. A top-level, package-private class with a no-arg (or plain-dependency) constructor can be instantiated directly in a test — `new CompanyConverter()` — with no Spring context, no `@WebMvcTest`, no mocking the controller just to reach the conversion logic. An inner class tied to the controller's instance would drag the controller (and its service, and the app context) into every test that just wants to check "does this request map to this domain object." `MixtureConverterTest` is the existing proof of this: it builds a `MixtureConverter` by hand with a mocked `FoodService` and a real `NutritionCalculator`, and tests the conversion/aggregation math in isolation — no controller, no HTTP layer, no Spring Boot test slice involved at all.

It's also not injected as a Spring bean (no `@Component`) — it's stateless and has no reason to be a singleton managed by the container; `new CompanyConverter()` inside the controller is enough. The controller's only job is: validate the shape of the request, call one service method, convert the result. If a controller method has an `if` statement that isn't about HTTP status codes, that logic is in the wrong layer — which is why `validateUpdateRequest` (at least one field must be present) lives in the converter, not the controller.

## Why this is worth having as a template

The value isn't the pattern itself — plenty of Spring codebases land somewhere similar. It's that I generate it the same way every time, for every entity, so:

- **A reviewer who knows the pattern can review by exception.** Once you've seen `Company` and `Food` built this way, reviewing the next entity is checking "does this deviate, and why" instead of reading nine files cold.
- **The boring failure modes get built in, not remembered.** Soft deletes, audit timestamps, page-size caps, sort whitelists, empty-list-not-null — these are exactly the things that get skipped under deadline pressure when each entity is built by hand. Here they're not optional steps; they're what the template produces.
- **Consistency compounds.** The tenth entity in a codebase built this way costs less to add, review, and maintain than the second — because nothing about it is a surprise.

I use this as a literal generation template — I keep it as a written specification and apply it entity by entity, which is also why it holds up as documentation: the spec and the code can't drift, because the spec is what generated the code.

## Questions

If you're building a Spring Boot backend and want to talk through whether this shape fits your domain — it doesn't fit everything, and I'll tell you where it doesn't — reach out.

[jeb.seibel@yahoo.com](mailto:jeb.seibel@yahoo.com)
