package rsnrdk.websec.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rsnrdk.websec.entity.Item;
import rsnrdk.websec.repository.ItemRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    void shouldCreateItem() {
        Item item = Item.builder()
                .name("Test item")
                .description("Test description")
                .build();

        Item savedItem = Item.builder()
                .id(1L)
                .name("Test item")
                .description("Test description")
                .build();

        when(itemRepository.save(item)).thenReturn(savedItem);

        Item result = itemService.createItem(item);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test item", result.getName());
        assertEquals("Test description", result.getDescription());

        verify(itemRepository).save(item);
    }

    @Test
    void shouldGetItem() {
        Item item = Item.builder()
                .id(1L)
                .name("Test item")
                .description("Test description")
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Item result = itemService.getItem(1L);

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