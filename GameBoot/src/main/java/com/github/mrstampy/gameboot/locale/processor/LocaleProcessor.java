package com.github.mrstampy.gameboot.locale.processor;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.locale.messages.LocaleMessage;
import com.github.mrstampy.gameboot.locale.messages.LocaleRegistry;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor;

@Component
public class LocaleProcessor extends AbstractGameBootProcessor<LocaleMessage> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private LocaleRegistry registry;

  @Override
  public String getType() {
    return LocaleMessage.TYPE;
  }

  @Override
  protected void validate(LocaleMessage message) throws Exception {
    if (isEmpty(message.getLanguageCode())) fail(LANG_CODE_MISSING, "Missing lang code");
  }

  @Override
  protected Response processImpl(LocaleMessage message) throws Exception {
    Long systemId = message.getSystemId();

    Locale locale = null;
    if (isNotEmpty(message.getCountryCode())) {
      locale = new Locale(message.getLanguageCode(), message.getCountryCode());
    } else {
      locale = new Locale(message.getLanguageCode());
    }

    log.debug("Changing locale for system id {} to {}", systemId, locale);

    registry.put(systemId, locale);

    return new Response(message, ResponseCode.SUCCESS);
  }

}
