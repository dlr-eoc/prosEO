# prosEO Ingestor

Version 0.0.1-SNAPSHOT

The component which ingests new products into the prosEO system (i. e. creates the necessary metadata in the database and initiates the product data transfer into the prosEO storage component); this components is designed for mass data ("bulk") ingestion as well as for the ingestion of individual products.
Run information:
    docker run -d --add-host=brainhost:<Docker Host IP/Name> -p 8080:8080 [<Registry Name>:<Registry Port>/]proseo-productclass-mgr:0.0.1-SNAPSHOT
    