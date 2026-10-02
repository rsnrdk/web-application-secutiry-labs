package rsnrdk.websec.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import rsnrdk.websec.dto.ItemDto;
import rsnrdk.websec.entity.Item;
import rsnrdk.websec.mapper.ItemMapper;
import rsnrdk.websec.repository.ItemRepository;

import java.math.BigDecimal;
import java.util.List;
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
    void createItem_validDto_returnsSavedItem() {
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
    void getItem_existingId_returnsItem() {
        Item item = Item.builder()
                .id(1L)
                .name("Test item")
                .description("Test description")
                .build();

        ItemDto itemDto = ItemDto.builder()
                .id(1L)
                .name("Test item")
                .description("Test description")
                .build();

        when(itemRepository.findById(1L))
                .thenReturn(Optional.of(item));

        when(itemMapper.toDto(item))
                .thenReturn(itemDto);

        ItemDto result = itemService.getItem(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
//        assertEquals(999L, result.getId());
        assertEquals("Test item", result.getName());
        assertEquals("Test description", result.getDescription());

        verify(itemRepository).findById(1L);
        verify(itemMapper).toDto(item);
    }

    @Test
    void deleteItem_existingId_deletesItem() {
        when(itemRepository.existsById(1L)).thenReturn(true);

        itemService.deleteItem(1L);

        verify(itemRepository).existsById(1L);
        verify(itemRepository).deleteById(1L);
    }

    @Test
    void getItem_notFound_throwsException() {
        when(itemRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> itemService.getItem(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Item not found", exception.getReason());

        verify(itemRepository).findById(999L);
    }

    @Test
    void deleteItem_notFound_throwsException() {
        when(itemRepository.existsById(999L))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> itemService.deleteItem(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Item not found", exception.getReason());

        verify(itemRepository).existsById(999L);
        verify(itemRepository, never()).deleteById(anyLong());
    }

    @Test
    void getAllItems_returnsMappedItems() {
        Item item1 = Item.builder()
                .id(1L)
                .name("Item 1")
                .description("Description 1")
                .price(BigDecimal.valueOf(100))
                .build();

        Item item2 = Item.builder()
                .id(2L)
                .name("Item 2")
                .description("Description 2")
                .price(BigDecimal.valueOf(200))
                .build();

        ItemDto dto1 = ItemDto.builder()
                .id(1L)
                .name("Item 1")
                .description("Description 1")
                .price(BigDecimal.valueOf(100))
                .build();

        ItemDto dto2 = ItemDto.builder()
                .id(2L)
                .name("Item 2")
                .description("Description 2")
                .price(BigDecimal.valueOf(200))
                .build();

        when(itemRepository.findAll()).thenReturn(List.of(item1, item2));
        when(itemMapper.toDto(item1)).thenReturn(dto1);
        when(itemMapper.toDto(item2)).thenReturn(dto2);

        List<ItemDto> result = itemService.getAllItems();

        assertEquals(2, result.size());
        assertEquals("Item 1", result.get(0).getName());
        assertEquals("Item 2", result.get(1).getName());

        verify(itemRepository).findAll();
        verify(itemMapper).toDto(item1);
        verify(itemMapper).toDto(item2);
    }

    @Test
    void updateItem_existingItem_returnsUpdatedItem() {
        ItemDto dto = ItemDto.builder()
                .id(1L)
                .name("Updated item")
                .description("Updated description")
                .price(BigDecimal.valueOf(500))
                .build();

        Item existingItem = Item.builder()
                .id(1L)
                .name("Old item")
                .description("Old description")
                .price(BigDecimal.valueOf(100))
                .build();

        ItemDto updatedDto = ItemDto.builder()
                .id(1L)
                .name("Updated item")
                .description("Updated description")
                .price(BigDecimal.valueOf(500))
                .build();

        when(itemRepository.findById(1L))
                .thenReturn(Optional.of(existingItem));

        when(itemRepository.save(existingItem))
                .thenReturn(existingItem);

        when(itemMapper.toDto(existingItem))
                .thenReturn(updatedDto);

        ItemDto result = itemService.updateItem(dto);

        assertEquals(1L, result.getId());
        assertEquals("Updated item", result.getName());
        assertEquals("Updated description", result.getDescription());
        assertEquals(BigDecimal.valueOf(500), result.getPrice());

        verify(itemRepository).findById(1L);
        verify(itemMapper).updateEntity(dto, existingItem);
        verify(itemRepository).save(existingItem);
        verify(itemMapper).toDto(existingItem);
    }

    @Test
    void updateItem_notFound_throwsException() {
        ItemDto dto = ItemDto.builder()
                .id(999L)
                .name("Updated item")
                .description("Updated description")
                .price(BigDecimal.valueOf(500))
                .build();

        when(itemRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> itemService.updateItem(dto)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Item not found", exception.getReason());

        verify(itemRepository).findById(999L);
        verify(itemRepository, never()).save(any());
        verify(itemMapper, never()).updateEntity(any(), any());
    }
}