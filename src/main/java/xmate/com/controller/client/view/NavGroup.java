package xmate.com.controller.client.view;

import java.util.List;

public record NavGroup(String slug, String title, List<NavColumn> children) {
}
