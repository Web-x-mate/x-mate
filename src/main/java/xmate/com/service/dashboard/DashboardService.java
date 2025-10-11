// src/main/java/xmate/com/service/dashboard/DashboardService.java
package xmate.com.service.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;            // 🔥 ADDED
import jakarta.persistence.PersistenceContext;     // 🔥 ADDED

import xmate.com.repo.catalog.CategoryRepository;  // 🔥 ADDED
import xmate.com.repo.catalog.ProductRepository;   // 🔥 ADDED
import xmate.com.repo.catalog.ProductVariantRepository; // 🔥 ADDED
import xmate.com.repo.catalog.ProductMediaRepository;   // 🔥 ADDED


import xmate.com.repo.discount.DiscountUsageRepository;
import xmate.com.repo.inventory.InventoryRepository;
import xmate.com.repo.procurement.PurchaseOrderRepository;
import xmate.com.repo.sales.OrderItemRepository;
import xmate.com.repo.sales.OrderRepository;
import xmate.com.repo.system.ActivityLogRepository;
import xmate.com.repo.system.RoleRepository;
import xmate.com.repo.system.UserRepository;
import xmate.com.repo.system.PermissionRepository;      // 🔥 ADDED

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final InventoryRepository invRepo;
    private final DiscountUsageRepository discUsageRepo;
    private final PurchaseOrderRepository poRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final ActivityLogRepository logRepo;

    // 🔥 ADDED: catalog repos
    private final CategoryRepository catRepo;
    private final ProductRepository prodRepo;
    private final ProductVariantRepository varRepo;
    private final ProductMediaRepository mediaRepo;

    // 🔥 ADDED: permission repo (đếm quyền cho RBAC)
    private final PermissionRepository permRepo;

    // 🔥 ADDED: dùng native SQL đơn giản cho Activity 24h
    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper om = new ObjectMapper();

    /** KPIs + Sparklines + Sales(3 chart) + Live tables + Catalog/RBAC/Activity */
    public String buildKpisAndSalesJson(LocalDate from, LocalDate to, String gran) {
        Map<String, Object> root = new LinkedHashMap<>();

        // ====== KPIs ======
        BigDecimal revenuePaid = orderRepo.sumPaidRevenue(from, to);
        long orders = orderRepo.countOrdersBetween(from, to);
        put(root, "kpis.revenuePaid", revenuePaid);
        put(root, "kpis.orders", orders);
        put(root, "kpis.lowStock", invRepo.countLowStock(5));
        put(root, "kpis.usersRoles", userRepo.count() + " / " + roleRepo.count());
        put(root, "kpis.logsToday", logRepo.countToday());
        put(root, "kpis.discountOrders", discUsageRepo.countOrdersUsedDiscount(from, to));
        long poOpen = poRepo.countByStatus().stream()
                .filter(r -> "SUBMITTED".equals(r.getLabel()) || "PARTIALLY_RECEIVED".equals(r.getLabel()))
                .mapToLong(r -> r.getValue() == null ? 0L : r.getValue())
                .sum();
        put(root, "kpis.poOpen", poOpen);

        // ====== Sparklines (rev, orders, aov) ======
        var revSeries = orderRepo.sparkRevenue(from, to);
        var ordSeries = orderRepo.sparkOrders(from, to);
        List<Double> aovSeries = new ArrayList<>();
        int n = Math.max(revSeries.size(), ordSeries.size());
        for (int i = 0; i < n; i++) {
            double r = i < revSeries.size() && revSeries.get(i) != null ? revSeries.get(i) : 0d;
            int o = i < ordSeries.size() && ordSeries.get(i) != null ? ordSeries.get(i) : 0;
            aovSeries.add(o > 0 ? r / o : 0d);
        }
        put(root, "sparklines.revenue", revSeries);
        put(root, "sparklines.orders", ordSeries);
        put(root, "sparklines.aov", aovSeries);
        put(root, "sparklines.lowStock", List.of());
        put(root, "sparklines.users", List.of());
        put(root, "sparklines.logs", List.of());
        put(root, "sparklines.discountOrders", List.of());
        put(root, "sparklines.poOpen", List.of());

        // ====== Sales charts ======
        // 1) Doanh thu theo thời gian
        var agg = orderRepo.aggregatePaidRevenue(from, to, gran);
        List<String> revLabels = new ArrayList<>();
        List<Double> revValues = new ArrayList<>();
        for (var row : agg) {
            revLabels.add(row.getLabel());
            revValues.add(row.getValue() == null ? 0d : row.getValue());
        }
        put(root, "revenueSeries.labels", revLabels);
        put(root, "revenueSeries.values", revValues);

        // 2) Phễu trạng thái đơn
        var funnel = orderRepo.funnel(from, to);
        List<String> fLabels = new ArrayList<>();
        List<Integer> fValues = new ArrayList<>();
        for (var r : funnel) {
            fLabels.add(r.getLabel());
            fValues.add(r.getValue() == null ? 0 : r.getValue());
        }
        put(root, "orderFunnel.labels", fLabels);
        put(root, "orderFunnel.values", fValues);

        // 3) Top sản phẩm/biến thể
        int TOP = 10;
        var top = orderItemRepo.topVariantsByQty(from, to, TOP);
        List<String> tLabels = new ArrayList<>();
        List<Integer> tValues = new ArrayList<>();
        for (var r : top) {
            tLabels.add(r.getName());
            tValues.add(r.getQty() == null ? 0 : r.getQty());
        }
        put(root, "topVariants.labels", tLabels);
        put(root, "topVariants.values", tValues);

        // ====== Live tables ======
        // Recent orders
        int RECENT_ORDERS = 10;
        var ro = orderRepo.recent(RECENT_ORDERS);
        List<Map<String, Object>> recentOrders = new ArrayList<>();
        for (var r : ro) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", r.getCode());
            m.put("customer", r.getCustomer());
            m.put("total", r.getTotal());
            m.put("paymentStatus", r.getPaymentStatus());
            m.put("shippingStatus", r.getShippingStatus());
            m.put("createdAt", r.getCreatedAt());
            recentOrders.add(m);
        }
        put(root, "recentOrders", recentOrders);

        // Low SKUs
        int LOW_THRESHOLD = 5, LOW_TOP = 10;
        var lows = invRepo.findLowStock(LOW_THRESHOLD, LOW_TOP);
        List<Map<String, Object>> lowSkus = new ArrayList<>();
        for (var r : lows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sku", r.getSku());
            m.put("name", r.getName());
            m.put("onHand", r.getOnHand());
            m.put("reserved", r.getReserved());
            lowSkus.add(m);
        }
        put(root, "lowSkus", lowSkus);

        // PO receiving
        int PO_TOP = 10;
        var prs = poRepo.receiving(PO_TOP);
        List<Map<String, Object>> poReceiving = new ArrayList<>();
        for (var r : prs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", r.getCode());
            m.put("supplier", r.getSupplier());
            m.put("status", r.getStatus());
            m.put("receivedQty", r.getReceivedQty());
            m.put("totalQty", r.getTotalQty());
            poReceiving.add(m);
        }
        put(root, "poReceiving", poReceiving);

        // ====== Catalog snapshot (bar chart) — labels + values ======  🔥 ADDED
        put(root, "catalog.labels", List.of("Categories", "Products", "Variants", "Media"));
        put(root, "catalog.values", List.of(
                catRepo.count(),
                prodRepo.count(),
                varRepo.count(),
                mediaRepo.count()
        ));

        // ====== RBAC (bar chart): Roles vs Permissions ======        🔥 ADDED
        put(root, "rbac.labels", List.of("Roles", "Permissions"));
        put(root, "rbac.values", List.of(
                roleRepo.count(),
                permRepo.count()
        ));

        // ====== Activity logs (24h) — pie chart by action ======      🔥 ADDED
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery("""
                SELECT a.action, COUNT(*) 
                FROM activity_logs a
                WHERE a.created_at >= (NOW() - INTERVAL 1 DAY)
                GROUP BY a.action
                ORDER BY a.action
            """).getResultList();

            List<String> actLabels = new ArrayList<>();
            List<Integer> actValues = new ArrayList<>();
            for (Object[] row : rows) {
                actLabels.add(Objects.toString(row[0], "UNKNOWN"));
                // COUNT(*) trả BIGINT → về Java là BigInteger/Long → convert sang int an toàn
                Number cnt = (Number) row[1];
                actValues.add(cnt == null ? 0 : cnt.intValue());
            }
            put(root, "activity.labels", actLabels);
            put(root, "activity.values", actValues);
        } catch (Exception ignore) {
            // fallback rỗng (không phá UI)
            put(root, "activity.labels", List.of());
            put(root, "activity.values", List.of());
        }
        int STOCK_TOP = 12;
        var stock = invRepo.topStock(STOCK_TOP);
        List<String> stkLabels = new ArrayList<>();
        List<Integer> stkOn    = new ArrayList<>();
        List<Integer> stkRes   = new ArrayList<>();
        for (var r : stock) {
            stkLabels.add(r.getLabel());
            stkOn.add(r.getOnHand() == null ? 0 : r.getOnHand());
            stkRes.add(r.getReserved() == null ? 0 : r.getReserved());
        }
        put(root, "stock.labels",   stkLabels);
        put(root, "stock.onHand",   stkOn);
        put(root, "stock.reserved", stkRes);

        var pos = poRepo.countByStatusBetween(from, to);
        List<String> poLabels = new ArrayList<>();
        List<Integer> poVals  = new ArrayList<>();
        for (var r : pos) {
            poLabels.add(r.getLabel());
            poVals.add(r.getValue() == null ? 0 : r.getValue());
        }
        put(root, "poStatus.labels", poLabels);
        put(root, "poStatus.values", poVals);

//        var seg = segmentRepo.distribution();
//        List<String> segLabels = new ArrayList<>();
//        List<Integer> segVals  = new ArrayList<>();
//        for (var r : seg) {
//            segLabels.add(r.getLabel());
//            segVals.add(r.getValue() == null ? 0 : r.getValue());
//        }
//        put(root, "segments.labels", segLabels);
//        put(root, "segments.values", segVals);

//        var loy = loyaltyRepo.sumPointsByTier();
//        List<String> loyLabels = new ArrayList<>();
//        List<Integer> loyVals  = new ArrayList<>();
//        for (var r : loy) {
//            loyLabels.add(r.getLabel());
//            loyVals.add(r.getValue() == null ? 0 : r.getValue());
//        }
//        put(root, "loyalty.labels", loyLabels);
//        put(root, "loyalty.values", loyVals);

        int DISC_TOP = 10;
        var eff = discUsageRepo.effectiveness(from, to, DISC_TOP);
        List<String> disLabels = new ArrayList<>();
        List<Integer> disOrders = new ArrayList<>();
        List<Double>  disRev    = new ArrayList<>();
        for (var r : eff) {
            disLabels.add(r.getLabel());
            disOrders.add(r.getOrders() == null ? 0 : r.getOrders());
            disRev.add(r.getRevenue() == null ? 0d : r.getRevenue());
        }
        put(root, "discountEffect.labels",  disLabels);
        put(root, "discountEffect.orders",  disOrders);
        put(root, "discountEffect.revenue", disRev);
        try {
            return om.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Serialize dashboard json failed", e);
        }

    }

    // helper
    @SuppressWarnings("unchecked")
    private static void put(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> cur = root;
        for (int i = 0; i < parts.length - 1; i++) {
            cur = (Map<String, Object>) cur.computeIfAbsent(parts[i], k -> new LinkedHashMap<>());
        }
        cur.put(parts[parts.length - 1], value);
    }
}
