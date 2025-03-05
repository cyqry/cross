package com.ytycc.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunLog {
    public static final Logger OPEN_LOGGER = LoggerFactory.getLogger("com.ytycc.open");

    public static final Logger CLOSE_LOGGER = LoggerFactory.getLogger("com.ytycc.close");

    public static final Logger ERROR_LOGGER = LoggerFactory.getLogger("com.ytycc.error");

    public static final Logger INFO_LOGGER = LoggerFactory.getLogger("com.ytycc.info");
}
