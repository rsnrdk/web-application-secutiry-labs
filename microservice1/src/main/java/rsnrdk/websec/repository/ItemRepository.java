package rsnrdk.websec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rsnrdk.websec.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {
}