package com.thecodinglab.imdbclone.enums.attributeconverter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

// could be used if its more useful to convert startYear/endYear integers into dates!
// add @Convert(converter = StartYearConverter.class)
@Converter
public class StartYearConverter implements AttributeConverter<Date, Integer> {

  @Override
  public Integer convertToDatabaseColumn(Date attribute) {
    Calendar calendar = new GregorianCalendar();
    if (attribute != null) {
      calendar.setTime(attribute);
      return calendar.get(Calendar.YEAR);
    } else {
      return null;
    }
  }

  @Override
  public Date convertToEntityAttribute(Integer dbData) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy");
    try {
      return (dbData != null) ? format.parse(dbData.toString()) : null;
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
