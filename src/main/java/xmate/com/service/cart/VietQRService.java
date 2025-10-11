package xmate.com.service.cart;

import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
public class VietQRService {

    public String generateQRCodeUrl(String bankBin, String accountNumber, String accountName, long amount, String description) throws UnsupportedEncodingException {
        String formattedDescription = removeAccents(description)
                .replaceAll("[^a-zA-Z0-9 ]", "");

        String formattedAccountName = removeAccents(accountName).toUpperCase();

        String url = String.format(
                "https://img.vietqr.io/image/%s-%s-compact.png?amount=%d&addInfo=%s&accountName=%s",
                bankBin,
                accountNumber,
                amount,
                URLEncoder.encode(formattedDescription, "UTF-8"),
                URLEncoder.encode(formattedAccountName, "UTF-8")
        );
        return url;
    }

    private String removeAccents(String str) {
        if (str == null) return "";
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").replaceAll("Đ", "D").replaceAll("đ", "d");
    }
}