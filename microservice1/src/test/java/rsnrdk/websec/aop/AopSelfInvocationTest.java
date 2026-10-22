package rsnrdk.websec.aop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.test.context.support.WithMockUser;
import rsnrdk.websec.entity.Item;
import rsnrdk.websec.repository.ItemRepository;
import rsnrdk.websec.service.ItemService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs with the real Spring context, because the self-invocation problem only exists
 * where Spring wraps beans into proxies.
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class AopSelfInvocationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    private Long newItemId(String name) {
        return itemRepository.save(Item.builder().name(name).build()).getId();
    }

    @Test
    void itemService_isWrappedInSpringProxy() {
        assertTrue(AopUtils.isAopProxy(itemService));
    }

    @Test
    void deleteItem_calledFromOutside_goesThroughProxyAndIsAudited(CapturedOutput output) {
        // Arrange
        Long id = newItemId("external-call");

        // Act: the test is "another bean" calling the service through the proxy
        itemService.deleteItem(id);

        // Assert
        assertFalse(itemRepository.existsById(id));
        assertTrue(output.getOut().contains("action=ITEM_DELETED user=admin method=ItemService.deleteItem(..) args=[" + id + "]"));
    }

    @Test
    void deleteItems_callsDeleteItemViaThis_itemsDeletedButAuditSilentlySkipped(CapturedOutput output) {
        // Arrange
        Long first = newItemId("self-call-1");
        Long second = newItemId("self-call-2");

        // Act: deleteItems() calls this.deleteItem(id) internally, bypassing the proxy
        itemService.deleteItems(List.of(first, second));

        // Assert: the work itself was done...
        assertFalse(itemRepository.existsById(first));
        assertFalse(itemRepository.existsById(second));
        // ...but the aspect never ran, and nothing reported an error
        assertFalse(output.getOut().contains("args=[" + first + "]"));
        assertFalse(output.getOut().contains("args=[" + second + "]"));
    }
}