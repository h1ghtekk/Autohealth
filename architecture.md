```mermaid
flowchart TD
    subgraph Presentation["Presentation Layer"]
        UI["UI Components"]
        ViewModel["ViewModels"]
    end

    subgraph Domain["Domain Layer"]
        UseCases["Use Cases"]
        DomainModels["Domain Models"]
        Repositories["Repository Interfaces"]
    end

    subgraph Data["Data Layer"]
        DataSources["Data Sources"]
        RepositoryImpl["Repository Implementations"]
        Network["Network"]
        LocalDB["Local Database"]
    end

    UI --> ViewModel
    ViewModel --> UseCases
    UseCases --> Repositories
    RepositoryImpl --> DataSources
    DataSources --> Network
    DataSources --> LocalDB
    Repositories --> RepositoryImpl

    classDef presentation fill:#f9f,stroke:#333,stroke-width:2px
    classDef domain fill:#bfb,stroke:#333,stroke-width:2px
    classDef data fill:#fbb,stroke:#333,stroke-width:2px

    class UI,ViewModel presentation
    class UseCases,DomainModels,Repositories domain
    class DataSources,RepositoryImpl,Network,LocalDB data
```

# Архитектура приложения Autohealth

## Описание слоев

### Presentation Layer (Слой представления)
- **UI Components**: Компоненты пользовательского интерфейса (Activities, Fragments, Composables)
- **ViewModels**: ViewModels для управления состоянием UI и обработки пользовательских действий

### Domain Layer (Доменный слой)
- **Use Cases**: Бизнес-логика приложения
- **Domain Models**: Основные модели данных
- **Repository Interfaces**: Интерфейсы для работы с данными

### Data Layer (Слой данных)
- **Data Sources**: Источники данных (локальные и удаленные)
- **Repository Implementations**: Реализации репозиториев
- **Network**: Сетевой слой для работы с API
- **Local Database**: Локальное хранилище данных

## Принципы архитектуры
1. Чистая архитектура с разделением на слои
2. Dependency Injection для управления зависимостями
3. Single Responsibility Principle
4. Repository Pattern для абстракции источников данных
5. MVVM паттерн в Presentation Layer 