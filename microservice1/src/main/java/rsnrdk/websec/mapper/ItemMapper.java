package rsnrdk.websec.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import rsnrdk.websec.dto.ItemDto;
import rsnrdk.websec.entity.Item;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemDto toDto(Item item);

    Item toEntity(ItemDto dto);

    void updateEntity(ItemDto dto, @MappingTarget Item item);
}