from fastapi import FastAPI
from pydantic import BaseModel

from app.services.embedding_service import EmbeddingService
from app.services.image_embedding_service import ImageEmbeddingService
from app.services.location_service import LocationService


app = FastAPI()

embedding_service = EmbeddingService()
image_embedding_service = ImageEmbeddingService()
location_service = LocationService()


class EmbeddingRequest(BaseModel):
    text: str



class ComplaintTextRequest(BaseModel):
    title1: str
    description1: str
    summary1: str
    category1: str

    title2: str
    description2: str
    summary2: str
    category2: str



class ImageEmbeddingRequest(BaseModel):
    image_url: str



class ImageSimilarityRequest(BaseModel):
    image_url1: str
    image_url2: str


class LocationSimilarityRequest(BaseModel):
    latitude1: float
    longitude1: float

    latitude2: float
    longitude2: float


class AddressSimilarityRequest(BaseModel):
    address1: str
    address2: str

@app.post("/api/v1/embeddings/text")
def generate_text_embedding(request: EmbeddingRequest):

    embedding = embedding_service.generate_embedding(
        request.text
    )

    return {
        "embedding": embedding
    }



@app.post("/api/v1/embeddings/complaint-similarity")
def compare_complaints(request: ComplaintTextRequest):

    result = embedding_service.compare_complaints(
        request.title1,
        request.description1,
        request.summary1,
        request.category1,
        request.title2,
        request.description2,
        request.summary2,
        request.category2
    )

    return result



@app.post("/api/v1/embeddings/image")
def generate_image_embedding(request: ImageEmbeddingRequest):

    embedding = image_embedding_service.generate_image_embedding(
        request.image_url
    )

    return {
        "embedding": embedding
    }



@app.post("/api/v1/embeddings/image-similarity")
def calculate_image_similarity(request: ImageSimilarityRequest):

    embedding1 = image_embedding_service.generate_image_embedding(
        request.image_url1
    )

    embedding2 = image_embedding_service.generate_image_embedding(
        request.image_url2
    )

    similarity = image_embedding_service.calculate_image_similarity(
        embedding1,
        embedding2
    )

    return {
        "similarity": similarity
    }



@app.post("/api/v1/similarity/location")
def calculate_location_distance(
        request: LocationSimilarityRequest
):

    distance = location_service.calculate_distance(
        request.latitude1,
        request.longitude1,
        request.latitude2,
        request.longitude2
    )

    return {
        "distance_meters": distance
    }



@app.post("/api/v1/similarity/address")
def calculate_address_similarity(
        request: AddressSimilarityRequest
):

    similarity = embedding_service.calculate_address_similarity(
        request.address1,
        request.address2
    )

    return {
        "similarity": similarity
    }