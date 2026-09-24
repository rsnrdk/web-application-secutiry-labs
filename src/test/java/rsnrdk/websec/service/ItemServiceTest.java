package rsnrdk.websec.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rsnrdk.websec.dto.ItemDto;
import rsnrdk.websec.entity.Item;
import rsnrdk.websec.mapper.ItemMapper;
import rsnrdk.websec.repository.ItemRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    @Mock
    private ItemMapper itemMapper;

    @Test
    void shouldCreateItem() {
        ItemDto dto = ItemDto.builder()
                .name("Test item")
                .description("Test description")
                .price(BigDecimal.valueOf(100000.10))
                .build();

        Item item = Item.builder()
                .name("Test item")
                .description("Test description")
                .price(BigDecimal.valueOf(100000.10))
                .build();

        Item savedItem = Item.builder()
                .id(1L)
                .name("Test item")
                .description("Test description")
                .price(BigDecimal.valueOf(100000.10))
                .build();

        ItemDto savedDto = ItemDto.builder()
                .id(1L)
                .name("Test item")
                .description("Test description")
                .price(BigDecimal.valueOf(100000.10))
                .build();

        when(itemMapper.toEntity(dto)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(savedItem);
        when(itemMapper.toDto(savedItem)).thenReturn(savedDto);

        ItemDto result = itemService.createItem(dto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test item", result.getName());
        assertEquals("Test description", result.getDescription());
        assertEquals(BigDecimal.valueOf(100000.10), result.getPrice());

        verify(itemMapper).toEntity(dto);
        verify(itemRepository).save(item);
        verify(itemMapper).toDto(savedItem);
    }

    @Test
    void shouldGetItem() {
        Item item = Item.builder()
                .id(1L)
                .name("Test item")
                .description("Test description")
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        ItemDto result = itemService.getItem(1L);

        assertEquals(1L, result.getId());
        assertEquals("Test item", result.getName());

        verify(itemRepository).findById(1L);
    }

    @Test
    void shouldDeleteItem() {
        when(itemRepository.existsById(1L)).thenReturn(true);

        itemService.deleteItem(1L);

        verify(itemRepository).existsById(1L);
        verify(itemRepository).deleteById(1L);
    }
}