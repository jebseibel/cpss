package com.seibel.cpss.database.db.mapper;

import com.seibel.cpss.common.domain.PasswordResetToken;
import com.seibel.cpss.database.db.entity.PasswordResetTokenDb;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PasswordResetTokenMapper {
    private final ModelMapper mapper = new ModelMapper();

    public PasswordResetToken toModel(PasswordResetTokenDb db) {
        return db == null ? null : mapper.map(db, PasswordResetToken.class);
    }

    public PasswordResetTokenDb toDb(PasswordResetToken model) {
        return model == null ? null : mapper.map(model, PasswordResetTokenDb.class);
    }

    public List<PasswordResetToken> toModelList(List<PasswordResetTokenDb> list) {
        return list == null ? List.of() : list.stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<PasswordResetTokenDb> toDbList(List<PasswordResetToken> list) {
        return list == null ? List.of() : list.stream().map(this::toDb).collect(Collectors.toList());
    }
}
