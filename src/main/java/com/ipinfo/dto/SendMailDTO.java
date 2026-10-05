package com.ipinfo.dto;

import java.util.List;

public record SendMailDTO(
         
        String to,
        String subject,
        String html,
        List<FileClientAppDto> files) {
}
