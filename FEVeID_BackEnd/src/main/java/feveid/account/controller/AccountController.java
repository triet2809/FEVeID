package feveid.account.controller;


import feveid.account.dto.AccountResponse;
import feveid.account.dto.CreateAccountRequest;
import feveid.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account Management", description = "Các API quản lý tài khoản thành viên trong FEVeID")
public class AccountController {

    private final AccountService accountService;

    // Phần này tự gen ra bằng cách click chuột vào ô bóng đèn
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới", description = "Tạo một tài khoản mới theo role mặc định là MEMBER")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin tài khoản", description = "Tìm kiếm tài khoản theo ID")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(response);
    }
    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả tài khoản", description = "Sử dụng cho HR/Admin/President để lấy danh sách toàn bộ tài khoản")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List <AccountResponse> response = accountService.getAllAccounts();
        return ResponseEntity.ok(response);
    }
}
