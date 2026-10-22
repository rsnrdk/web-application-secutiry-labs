package rsnrdk.websec.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rsnrdk.websec.dto.ItemDto;
import rsnrdk.websec.entity.Item;
import rsnrdk.websec.mapper.ItemMapper;
import rsnrdk.websec.repository.ItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public List<ItemDto> getAllItems() {
        return itemRepository.findAll()
                .stream()
                .map(itemMapper::toDto)
                .toList();
    }

    public ItemDto getItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Item not found"
                        )
                );

        return itemMapper.toDto(item);
    }

    public ItemDto createItem(ItemDto dto) {
        Item item = itemMapper.toEntity(dto);

        item.setId(null);

        Item savedItem = itemRepository.save(item);

        return itemMapper.toDto(savedItem);
    }

    public ItemDto updateItem(ItemDto dto) {
        Item existingItem = itemRepository.findById(dto.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Item not found"
                        )
                );

        itemMapper.updateEntity(dto, existingItem);

        Item updatedItem = itemRepository.save(existingItem);

        return itemMapper.toDto(updatedItem);
    }

    public void deleteItem(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Item not found"
            );
        }

        itemRepository.deleteById(id);
    }
}