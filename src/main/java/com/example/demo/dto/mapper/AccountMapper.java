package com.example.demo.dto.mapper;

import com.example.demo.dto.AccountDto;
import com.example.demo.entity.Account;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

  AccountDto entityToDTO(Account account);

  List<AccountDto> entityToDTO(Iterable<Account> movie);

  @Mapping(target = "id", ignore = true)
  Account dtoToEntity(AccountDto movie);

  List<Account> dtoToEntity(Iterable<AccountDto> movie);
}
