package com.thecodinglab.imdbclone.support;

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@AutoConfigureRestTestClient
@AutoConfigureMockMvc
public abstract class BaseControllerIntegrationTest extends BaseContainers {}
