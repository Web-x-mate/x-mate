package xmate.com.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import xmate.com.dto.auth.ForgotReq;
import xmate.com.dto.auth.ResetPasswordReq;
import xmate.com.service.auth.ForgotPasswordService;

@Controller
@RequiredArgsConstructor
public class ForgotController {
    private final ForgotPasswordService svc;
    private final PasswordEncoder encoder;

    @PostMapping("/auth/forgot")
    public String forgot(@ModelAttribute ForgotReq req, Model model){
        try{
            var data = svc.start(req);
            model.addAttribute("otpId", data.get("otpId"));
            model.addAttribute("channel", data.get("channel"));
            model.addAttribute("destination", data.get("destination"));
            return "auth/otp"; // trang nhập OTP
        }catch (Exception e){
            model.addAttribute("error", e.getMessage());
            return "auth/forgot"; // trang bạn đã có
        }
    }

    @PostMapping("/auth/forgot/verify")
    public String verify(@RequestParam String otpId, @RequestParam String code, Model model){
        try{
            var token = svc.verify(otpId, code);
            model.addAttribute("token", token);
            return "auth/reset-password";
        }catch (Exception e){
            model.addAttribute("otpId", otpId);
            model.addAttribute("error", e.getMessage());
            return "auth/otp";
        }
    }

    @PostMapping("/auth/reset")
    public String reset(@ModelAttribute ResetPasswordReq req, Model model){
        try{
            svc.reset(req.token(), encoder.encode(req.newPassword()));
            model.addAttribute("msg","Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
            return "auth/login";
        }catch (Exception e){
            model.addAttribute("token", req.token());
            model.addAttribute("error", e.getMessage());
            return "auth/reset-password";
        }
    }
    @PostMapping("/auth/forgot/resend")
    public String resend(@RequestParam String otpId, Model model){
        try{
            var data = svc.resend(otpId);
            model.addAttribute("otpId", data.get("otpId"));
            model.addAttribute("channel", data.get("channel"));
            model.addAttribute("destination", data.get("destination"));
            model.addAttribute("msg", "Đã gửi lại OTP.");
            return "auth/otp";
        }catch (Exception e){
            model.addAttribute("otpId", otpId);
            model.addAttribute("error", e.getMessage());
            return "auth/otp";
        }
    }

}
