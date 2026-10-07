package com.aricansoft.sahatakip.data.model

enum class ProgressStatus(val label: String) {
    NOT_STARTED("Başlanmadı"),
    IN_PROGRESS("Devam"),
    FINISHED("Bitti")
}

enum class QualityStatus(val label: String) {
    NOT_EVALUATED("Değerlendirilmedi"),
    CONFORMING("Uygun"),
    DEFECTIVE("Kusurlu"),
    CRITICAL_DEFECT("Ağır kusurlu")
}

enum class ControlStatus(val label: String) {
    NOT_CHECKED("Kontrol edilmedi"),
    CANNOT_CHECK("Kontrol edilemedi"),
    CHECKED("Kontrol edildi"),
    ACCEPTED("Kabul")
}

enum class WorkItemScope(val label: String) {
    THIS_BLOCK("Yalnız bu blok"),
    BLOCK_TYPE("Aynı blok tipindeki tüm bloklar"),
    PROJECT("Projedeki tüm bloklar")
}

enum class WorkItemKind(val label:String) {
    ELECTRICAL("Elektrik imalatı"),
    RELATED_DISCIPLINE("Alakadar başka disiplin kalemi")
}

enum class FindingKind(val label:String) {
    PROBLEM("Problem"),
    ADVANTAGE("Avantaj")
}

enum class ProblemRecordStatus {
    OPEN,
    CLOSED
}

enum class DeficiencyStatus(val label:String) {
    OPEN("Açık"),
    IN_PROGRESS("Gideriliyor"),
    FIXED("Giderildi"),
    VERIFIED("Kontrol edildi")
}

enum class DeficiencyPriority(val label:String) {
    NORMAL("Normal"),
    HIGH("Yüksek"),
    CRITICAL("Kritik")
}

enum class AuditEventType {
    CREATED,
    STATUS_CHANGED,
    NOTE_ADDED,
    PROBLEM_OPENED,
    PROBLEM_CLOSED,
    ADVANTAGE_OPENED,
    ADVANTAGE_CLOSED,
    DEFICIENCY_CREATED,
    DEFICIENCY_STATUS_CHANGED,
    PHOTO_ADDED
}
