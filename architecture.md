graph TD
    A[MainActivity] --> B[BaseSensorActivity]
    B --> C[SensorsActivity]
    B --> D[EngineTemperatureActivity]
    B --> E[ThrottlePositionActivity]
    B --> F[EngineRpmActivity]
    B --> G[MassAirflowActivity]
    B --> H[VehicleSpeedActivity]
    B --> I[TemperatureSensorActivity]
    
    A --> J[HistoryActivity]
    A --> K[BtCommService]
    
    L[Database] --> A
    L --> J
    
    M[Fragments] --> A
    N[Adapters] --> A
    N --> J
    
    K --> B
    K --> O[TroubleCodes]
