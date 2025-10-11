package xmate.com.util;

public class IdMask {
    public static boolean looksLikeEmail(String s){ return s!=null && s.contains("@"); }
    public static String normalizePhone(String raw){ return raw==null? null : raw.replaceAll("[^0-9]",""); }
    public static String maskEmail(String email){
        var p=email.split("@"); var name=p[0];
        return name.substring(0, Math.min(2,name.length())) + "***@" + p[1];
    }
    public static String maskPhone(String p){
        var n = normalizePhone(p);
        if(n==null || n.length()<4) return "***";
        return "*".repeat(n.length()-4) + n.substring(n.length()-4);
    }
}
