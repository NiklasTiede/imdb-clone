FROM python:3.14-slim

WORKDIR /app

COPY app/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

COPY app/seed.py /app/seed.py
COPY seed /seed

ENTRYPOINT ["python", "/app/seed.py"]
CMD ["all"]
