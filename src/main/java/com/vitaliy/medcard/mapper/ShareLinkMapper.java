package com.vitaliy.medcard.mapper;

import com.vitaliy.medcard.dto.ShareLinkResponseDto;
import com.vitaliy.medcard.model.ShareLink;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShareLinkMapper {

    ShareLinkResponseDto toDto(ShareLink shareLink);
}
