package com.thecodinglab.imdbclone.payload.mapper;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.payload.account.AccountRecord;
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
}
