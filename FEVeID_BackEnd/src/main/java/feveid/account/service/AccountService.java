package feveid.account.service;

import feveid.account.dto.AccountResponse;
import feveid.account.dto.CreateAccountRequest;

import java.util.List;

public interface AccountService {
    AccountResponse createAccount(CreateAccountRequest request);



    AccountResponse getAccountById(Long id);

    List<AccountResponse> getAllAccounts();

}
