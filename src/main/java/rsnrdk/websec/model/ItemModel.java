package rsnrdk.websec.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ItemModel {
    private String id;
    private String name;
    private String description;

    public ItemModel(String description, String name) {
        this.description = description;
        this.name = name;
    }
}
