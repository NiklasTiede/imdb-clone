package com.example.demo.payload.mapper;

import com.example.demo.entity.Account;
import com.example.demo.payload.AccountRecord;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

  AccountRecord entityToDTO(Account account);

  List<AccountRecord> entityToDTO(Iterable<Account> accounts);

  @Mapping(target = "id", ignore = true)
  Account dtoToEntity(AccountRecord account);

  List<Account> dtoToEntity(Iterable<AccountRecord> accounts);
}
