package com.fruitisland.game.service.impl;

import com.fruitisland.common.base.BaseServiceImplX;
import com.fruitisland.game.entity.CustomerTemplate;
import com.fruitisland.game.mapper.CustomerTemplateMapper;
import com.fruitisland.game.service.CustomerTemplateService;
import org.springframework.stereotype.Service;

@Service
public class CustomerTemplateServiceImpl extends BaseServiceImplX<CustomerTemplateMapper, CustomerTemplate> implements CustomerTemplateService {
}
