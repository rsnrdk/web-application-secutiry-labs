package rsnrdk.websec.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import rsnrdk.websec.dto.ItemDto;
import rsnrdk.websec.service.ItemService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

    @Mock
    private ItemService itemService;

    @InjectMocks
    private ItemController itemController;

    @Test
    void getAll_itemsExist_returnsItemsFromService() {
        List<ItemDto> items = List.of(ItemDto.builder().id(1L).name("A").build());
        when(itemService.getAllItems()).thenReturn(items);

        List<ItemDto> result = itemController.getAll();

        assertSame(items, result);
    }

    @Test
    void getItem_existingId_returnsItem() {
        ItemDto item = ItemDto.builder().id(5L).name("Laptop").price(BigDecimal.TEN).build();
        when(itemService.getItem(5L)).thenReturn(item);

        ItemDto result = itemController.getItem(5L);

        assertEquals("Laptop", result.getName());
    }

    @Test
    void getItem_missingId_propagatesNotFound() {
        when(itemService.getItem(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> itemController.getItem(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void createItem_validDto_delegatesToServiceAndReturnsCreated() {
        ItemDto request = ItemDto.builder().name("New").build();
        ItemDto created = ItemDto.builder().id(10L).name("New").build();
        when(itemService.createItem(request)).thenReturn(created);

        ItemDto result = itemController.createItem(request);

        assertEquals(10L, result.getId());
        verify(itemService).createItem(request);
    }

    @Test
    void updateItem_validDto_delegatesToServiceAndReturnsUpdated() {
        ItemDto request = ItemDto.builder().id(3L).name("Renamed").build();
        when(itemService.updateItem(request)).thenReturn(request);

        ItemDto result = itemController.updateItem(request);

        assertEquals("Renamed", result.getName());
        verify(itemService).updateItem(request);
    }

    @Test
    void deleteItem_existingId_callsServiceWithSameId() {
        itemController.deleteItem(7L);

        verify(itemService).deleteItem(7L);
    }

    @Test
    void deleteItem_missingId_propagatesNotFound() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"))
                .when(itemService).deleteItem(99L);

        assertThrows(ResponseStatusException.class, () -> itemController.deleteItem(99L));
    }
}