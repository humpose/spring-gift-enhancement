package gift.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Embeddable
public class OptionQuantity {

    private int quantity;

    protected OptionQuantity() {
    }

    public OptionQuantity(int quantity) {
        if (quantity < 1 || quantity > 99999999) {
            throw new IllegalArgumentException("옵션 수량은 1개 이상 1억 개 미만이어야 합니다.");
        }
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }
}