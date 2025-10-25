package xmate.com.controller.client.view;

import java.util.List;

public record NavColumn(String slug, String title, List<NavItem> items) {
}
