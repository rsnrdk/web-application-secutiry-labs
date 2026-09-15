package rsnrdk.websec.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rsnrdk.websec.model.ItemModel;
import rsnrdk.websec.service.ItemService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemModel> getAll(){
        return itemService.getAllItems();
    }

    @GetMapping("/{id}")
    public ItemModel getItem(@PathVariable String id){
        return itemService.getItem(id);
    }

    @PostMapping
    public ItemModel createItem(@RequestBody ItemModel item){
        return itemService.createItem(item);
    }

    @PutMapping
    public ItemModel updateItem(@RequestBody ItemModel item){
        return itemService.updateItem(item);
    }

    @DeleteMapping("/{id}")
    public   void deleteItem(@PathVariable String id){
        itemService.deleteItem(id);
    }
}
