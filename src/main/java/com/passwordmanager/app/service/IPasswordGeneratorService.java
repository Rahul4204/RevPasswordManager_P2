package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.PasswordGeneratorConfigDTO;
import java.util.List;

public interface IPasswordGeneratorService {
    List<String> generate(PasswordGeneratorConfigDTO config);

    int strengthScore(String password);

    String strengthLabel(int score);
}
