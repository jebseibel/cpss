package com.seibel.cpss.service;

import com.seibel.cpss.common.domain.Nutrition;
import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.service.NutritionDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class NutritionService extends BaseService {

    private final NutritionDbService dbService;
    private final String thisName = "Nutrition";

    public NutritionService(NutritionDbService dbService) {
        super(Nutrition.class.getSimpleName());
        this.dbService = dbService;
    }

    public Nutrition create(Nutrition item) {
        requireNonNull(item, thisName);
        log.info("Creating {}", thisName);
        return dbService.create(item);
    }

    public Nutrition update(String extid, Nutrition item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, thisName);
        log.info("Updating {} {}", thisName, extid);
        return dbService.update(extid, item);
    }

    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("Deleting {} {}", thisName, extid);
        return dbService.delete(extid);
    }

    public Nutrition findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        return dbService.findByExtid(extid);
    }

    public List<Nutrition> findAll() {
        return dbService.findAll();
    }

    public List<Nutrition> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        return dbService.findByActive(activeEnum);
    }
}
