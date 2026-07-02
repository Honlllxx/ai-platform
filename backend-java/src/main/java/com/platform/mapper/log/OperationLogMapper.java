package com.platform.mapper.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.entity.log.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {}
