package com.vitaliy.medcard.mapper;

import com.vitaliy.medcard.dto.DocumentResponseDto;
import com.vitaliy.medcard.model.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "uploadedByFullName", source = "uploadedBy.fullName")
    DocumentResponseDto toDto(Document document);
}
