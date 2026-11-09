"""AgriPulse AI service: crop disease classification (preliminary assessments only)."""
import io

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from PIL import Image, UnidentifiedImageError
from starlette.concurrency import run_in_threadpool
from transformers import MobileNetV2ImageProcessor, pipeline

MODEL_ID = "linkanjarad/mobilenet_v2_1.0_224-plant-disease-identification"
MAX_BYTES = 5 * 1024 * 1024
MIN_CONFIDENCE = 0.60
ALLOWED = {"image/jpeg", "image/png", "image/webp"}

# Declared crop -> words that appear in the model's labels for that crop.
# Only crops the model was trained on. Tobacco, wheat etc. are deliberately absent.
SUPPORTED = {
    "maize": ["corn", "maize"], "tomato": ["tomato"], "potato": ["potato"],
    "pepper": ["pepper"], "grape": ["grape"], "apple": ["apple"], "cherry": ["cherry"],
    "peach": ["peach"], "strawberry": ["strawberry"], "squash": ["squash"],
    "soybean": ["soybean"], "blueberry": ["blueberry"], "orange": ["orange"],
    "raspberry": ["raspberry"],
}
LIMITS = ("Trained on lab photos of a limited set of crops, not on Zimbabwean field conditions. "
          "Not a confirmed diagnosis.")

app = FastAPI(title="AgriPulse AI service")
classifier = pipeline("image-classification", model=MODEL_ID,
                      image_processor=MobileNetV2ImageProcessor())


def clean(label: str) -> str:
    return label.replace("___", " - ").replace("_", " ").strip()


def reply(status, advice, predictions=None, crop=None):
    return {"status": status, "crop": crop, "predictions": predictions or [], "advice": advice,
            "model": MODEL_ID, "limitations": LIMITS}


@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_ID, "supported_crops": sorted(SUPPORTED)}


@app.post("/classify")
async def classify(file: UploadFile = File(...), crop: str = Form(...)):
    crop = crop.strip().lower()
    if crop not in SUPPORTED:
        return reply("UNSUPPORTED_CROP",
                     "This tool cannot check that crop yet. Supported: " + ", ".join(sorted(SUPPORTED))
                     + ". Please show the plant to an agricultural extension officer.", crop=crop)
    if file.content_type not in ALLOWED:
        raise HTTPException(400, "Upload a JPEG, PNG or WebP image")
    data = await file.read(MAX_BYTES + 1)
    if len(data) > MAX_BYTES:
        raise HTTPException(413, "Image is larger than 5 MB")
    try:
        img = Image.open(io.BytesIO(data)).convert("RGB")
    except (UnidentifiedImageError, OSError):
        raise HTTPException(400, "File is not a readable image")

    results = await run_in_threadpool(classifier, img, top_k=3)
    top = results[0]
    if not any(w in top["label"].lower() for w in SUPPORTED[crop]):
        return reply("CROP_MISMATCH",
                     f"This photo does not look like a {crop} leaf to the model, so no result is shown. "
                     "Take a close-up photo of a single {crop} leaf in daylight, or ask an extension officer."
                     .replace("{crop}", crop), crop=crop)

    preds = [{"label": clean(r["label"]), "confidence": round(r["score"], 3)} for r in results]
    if top["score"] < MIN_CONFIDENCE:
        return reply("LOW_CONFIDENCE",
                     "The model is not confident. Take a clearer, close-up photo of one leaf in daylight, "
                     "and show it to an agricultural extension officer.", preds, crop)
    return reply("OK", "Preliminary assessment only. Confirm with an agricultural extension officer "
                       "before treating.", preds, crop)
