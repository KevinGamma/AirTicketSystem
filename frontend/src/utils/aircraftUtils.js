
const aircraftModels = {
  "B737-800": { englishName: "Boeing 737-800", chineseName: "波音737-800", totalSeats: 189 },
  "B737-900": { englishName: "Boeing 737-900", chineseName: "波音737-900", totalSeats: 215 },
  "B777-200": { englishName: "Boeing 777-200", chineseName: "波音777-200", totalSeats: 314 },
  "B777-300ER": { englishName: "Boeing 777-300ER", chineseName: "波音777-300ER", totalSeats: 396 },
  "B787-8": { englishName: "Boeing 787-8", chineseName: "波音787-8", totalSeats: 242 },
  "B787-9": { englishName: "Boeing 787-9", chineseName: "波音787-9", totalSeats: 290 },
  "A320": { englishName: "Airbus A320", chineseName: "空客A320", totalSeats: 180 },
  "A321": { englishName: "Airbus A321", chineseName: "空客A321", totalSeats: 220 },
  "A330-200": { englishName: "Airbus A330-200", chineseName: "空客A330-200", totalSeats: 293 },
  "A330-300": { englishName: "Airbus A330-300", chineseName: "空客A330-300", totalSeats: 335 },
  "A350-900": { englishName: "Airbus A350-900", chineseName: "空客A350-900", totalSeats: 325 },
  "A380": { englishName: "Airbus A380", chineseName: "空客A380", totalSeats: 525 },
  "CRJ-900": { englishName: "Bombardier CRJ-900", chineseName: "庞巴迪CRJ-900", totalSeats: 90 },
  "E190": { englishName: "Embraer E190", chineseName: "巴航工业E190", totalSeats: 114 },
  "ATR72": { englishName: "ATR 72", chineseName: "ATR72涡桨客机", totalSeats: 78 }
}


export function getAircraftDisplayName(aircraftType, language = 'zh') {
  if (!aircraftType) return ''
  
  const model = aircraftModels[aircraftType]
  if (model) {
    if (language === 'en') {
      return `${model.englishName} (${aircraftType})`
    } else {
      return `${model.chineseName} (${aircraftType})`
    }
  }

  return aircraftType
}


export function getAircraftModel(aircraftType) {
  return aircraftModels[aircraftType] || null
}


export function getAllAircraftModels() {
  return aircraftModels
}

export default {
  getAircraftDisplayName,
  getAircraftModel,
  getAllAircraftModels
}