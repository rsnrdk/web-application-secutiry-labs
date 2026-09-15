package rsnrdk.websec.service;

import org.springframework.stereotype.Service;
import rsnrdk.websec.model.ItemModel;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItemService {
    private final List<ItemModel> items = new ArrayList<>();

    {
        items.add(new ItemModel("1", "name1", "description1"));
        items.add(new ItemModel("2", "name2", "description2"));
        items.add(new ItemModel("3", "name3", "description3"));
    }

    public List<ItemModel> getAllItems() {
        return this.items;
    }

    public ItemModel createItem(ItemModel item){
        items.add(item);
        System.out.println(items.size());
        return item;
    }

    public ItemModel getItem(String id){
        return items.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst().orElse(null);
    }

    public ItemModel updateItem(ItemModel item){
        ItemModel oldItem = getItem(item.getId());
        items.remove(oldItem);
        items.add(item);
        return item;
    }

    public void deleteItem(String id){
        ItemModel item = getItem(id);
        items.remove(item);
    }
}
