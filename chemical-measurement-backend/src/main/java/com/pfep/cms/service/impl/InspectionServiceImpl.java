package com.pfep.cms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pfep.cms.entity.InspectionRecord;
import com.pfep.cms.mapper.InspectionRecordMapper;
import com.pfep.cms.service.InspectionService;
import org.springframework.stereotype.Service;

@Service
public class InspectionServiceImpl extends ServiceImpl<InspectionRecordMapper, InspectionRecord> implements InspectionService {
}
