package xmate.com.repo.sales;

public interface RecentOrderRow {
    Object getCode();

    Object getCustomer();

    Object getTotal();

    Object getPaymentStatus();

    Object getShippingStatus();

    Object getCreatedAt();
}
