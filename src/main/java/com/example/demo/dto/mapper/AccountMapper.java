package com.example.demo.dto.mapper;

import com.example.demo.dto.AccountRecord;
import com.example.demo.entity.Account;
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
