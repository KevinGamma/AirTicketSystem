package com.airticket.service;

import com.airticket.model.AircraftModel;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AircraftModelService {
    
    private final List<AircraftModel> aircraftModels = Arrays.asList(
        new AircraftModel("B737-800", "Boeing 737-800", "波音737-800", 189, "单通道窄体客机，适合中短程航线"),
        new AircraftModel("B737-900", "Boeing 737-900", "波音737-900", 215, "单通道窄体客机，增长版本"),
        new AircraftModel("B777-200", "Boeing 777-200", "波音777-200", 314, "双通道宽体客机，适合长程航线"),
        new AircraftModel("B777-300ER", "Boeing 777-300ER", "波音777-300ER", 396, "双通道宽体客机，超远程版本"),
        new AircraftModel("B787-8", "Boeing 787-8", "波音787-8", 242, "梦想客机，新一代宽体机"),
        new AircraftModel("B787-9", "Boeing 787-9", "波音787-9", 290, "梦想客机，加长版本"),

        new AircraftModel("A320", "Airbus A320", "空客A320", 180, "单通道窄体客机，欧洲制造"),
        new AircraftModel("A321", "Airbus A321", "空客A321", 220, "单通道窄体客机，加长版本"),
        new AircraftModel("A330-200", "Airbus A330-200", "空客A330-200", 293, "双通道宽体客机，中远程"),
        new AircraftModel("A330-300", "Airbus A330-300", "空客A330-300", 335, "双通道宽体客机，加长版本"),
        new AircraftModel("A350-900", "Airbus A350-900", "空客A350-900", 325, "新一代宽体客机，碳纤维机身"),
        new AircraftModel("A380", "Airbus A380", "空客A380", 525, "双层超大型客机，世界最大"),

        new AircraftModel("CRJ-900", "Bombardier CRJ-900", "庞巴迪CRJ-900", 90, "支线客机，适合短程航线"),
        new AircraftModel("E190", "Embraer E190", "巴航工业E190", 114, "支线客机，巴西制造"),
        new AircraftModel("ATR72", "ATR 72", "ATR72涡桨客机", 78, "涡桨支线客机，适合短程航线")
    );
    
    public List<AircraftModel> getAllAircraftModels() {
        return aircraftModels;
    }
    
    public AircraftModel getByCode(String code) {
        return aircraftModels.stream()
                .filter(model -> model.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
    
    public Map<String, AircraftModel> getAircraftModelMap() {
        return aircraftModels.stream()
                .collect(Collectors.toMap(AircraftModel::getCode, model -> model));
    }
}