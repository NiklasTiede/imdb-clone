package app.popcornsociety.account.internal.mapper;

import app.popcornsociety.account.api.AccountRecord;
import app.popcornsociety.account.api.UpdatedAccountProfile;
import app.popcornsociety.account.internal.persistence.Account;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper {

  AccountRecord entityToDTO(Account account);

  List<AccountRecord> entityToDTO(Iterable<Account> accounts);

  @Mapping(target = "id", ignore = true)
  Account dtoToEntity(AccountRecord account);

  List<Account> dtoToEntity(Iterable<AccountRecord> accounts);

  UpdatedAccountProfile entityToUpdatedProfile(Account account);
}
