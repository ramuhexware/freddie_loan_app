$services = @(
    "api-gateway",
    "customer-service",
    "loan-origination-service",
    "underwriting-service",
    "document-service",
    "notification-service",
    "messaging-service",
    "legacy-adapter-service",
    "card-service"
)

foreach ($service in $services) {
    $filePath = "C:\ramu\Project_Assignment\RapidX\FreddeMac_Project_RapidX\Freddie_Style_Application\freddie-loan-platform\$service\src\main\resources\application.properties"
    if (Test-Path $filePath) {
        $content = Get-Content $filePath -Raw
        $newLine = "eureka.client.service-url.defaultZone=`${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka}"
        
        if ($content -match "eureka\.client\.service-url\.defaultZone") {
            # Replace existing
            $content = $content -replace "eureka\.client\.service-url\.defaultZone=.*", $newLine
            Write-Host "Updated $service application.properties"
        } else {
            # Append
            $content = $content + "`n" + $newLine + "`n"
            Write-Host "Appended to $service application.properties"
        }
        Set-Content $filePath $content
    } else {
        Write-Warning "File not found: $filePath"
    }
}
