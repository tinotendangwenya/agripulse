-- Expert-reviewed knowledge base. Farmers only ever see rows with review_status = 'REVIEWED'.
CREATE TABLE disease_conditions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  subject VARCHAR(30) NOT NULL,
  name VARCHAR(150) NOT NULL,
  description TEXT,
  review_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  reviewed_by BIGINT NULL,
  reviewed_at TIMESTAMP NULL,
  review_notes VARCHAR(500),
  source_note VARCHAR(300),
  CONSTRAINT fk_cond_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE TABLE condition_symptoms (
  condition_id BIGINT NOT NULL,
  symptom VARCHAR(40) NOT NULL,
  PRIMARY KEY (condition_id, symptom),
  CONSTRAINT fk_cs_cond FOREIGN KEY (condition_id) REFERENCES disease_conditions(id)
);

CREATE TABLE condition_treatments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  condition_id BIGINT NOT NULL,
  active_ingredient VARCHAR(100) NULL,
  category VARCHAR(20) NOT NULL,
  advice VARCHAR(600) NOT NULL,
  CONSTRAINT fk_ct_cond FOREIGN KEY (condition_id) REFERENCES disease_conditions(id)
);

CREATE TABLE supplier_products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_id BIGINT NOT NULL,
  name VARCHAR(150) NOT NULL,
  active_ingredient VARCHAR(100) NOT NULL,
  concentration VARCHAR(60),
  category VARCHAR(20) NOT NULL,
  pack_size DECIMAL(10,2) NOT NULL,
  pack_unit VARCHAR(5) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  stock_packs INT NOT NULL DEFAULT 0,
  province VARCHAR(60) NOT NULL,
  district VARCHAR(80),
  latitude DECIMAL(9,6),
  longitude DECIMAL(9,6),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sp_supplier FOREIGN KEY (supplier_id) REFERENCES users(id),
  CONSTRAINT chk_sp_currency CHECK (currency IN ('USD','ZIG','ZAR')),
  CONSTRAINT chk_sp_unit CHECK (pack_unit IN ('L','KG')),
  INDEX idx_sp_ingredient (status, active_ingredient)
);

-- A log of every symptom check, so advice can be reviewed later. Photos and personal details are not stored.
CREATE TABLE symptom_checks (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  farm_id BIGINT NULL,
  subject VARCHAR(30) NOT NULL,
  symptoms VARCHAR(500),
  top_condition_id BIGINT NULL,
  outcome VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sc_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_sc_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE SET NULL
);

-- STARTER CONTENT: every row is DRAFT. Written from general agronomic knowledge, not from Zimbabwean guidelines.
INSERT INTO disease_conditions (id, subject, name, description, source_note) VALUES
(1,'maize','Common rust','Fungal disease. Small raised orange-brown pustules on both leaf surfaces that later turn dark. Heavy infection yellows and dries the leaves.','DRAFT from general knowledge. Not yet checked against Zimbabwean guidelines or registered products.'),
(2,'maize','Grey leaf spot','Fungal disease. Narrow rectangular tan to grey lesions running between the veins, worse in warm, humid weather.','DRAFT from general knowledge. Not yet checked against Zimbabwean guidelines or registered products.'),
(3,'maize','Northern leaf blight','Fungal disease. Long cigar-shaped grey-green to tan lesions on the leaves.','DRAFT from general knowledge. Not yet checked against Zimbabwean guidelines or registered products.'),
(4,'maize','Fall armyworm','Caterpillar pest. Ragged holes in the leaves and sawdust-like droppings in the whorl. Young plants can be badly damaged.','DRAFT from general knowledge. Not yet checked against Zimbabwean guidelines or registered products.'),
(5,'maize','Maize streak virus','Virus spread by leafhoppers. Pale yellow streaks along the leaves and stunted plants. There is no cure once a plant is infected.','DRAFT from general knowledge. Not yet checked against Zimbabwean guidelines or registered products.'),
(6,'tomato','Early blight','Fungal disease. Brown spots with rings in a target pattern, usually on older leaves first, with yellowing around the spots.','DRAFT from general knowledge. Not yet checked against Zimbabwean guidelines or registered products.'),
(7,'tomato','Late blight','Disease favoured by cool, wet weather. Dark water-soaked patches on leaves, white fuzz underneath in damp conditions, and spots on fruit. Spreads fast.','DRAFT from general knowledge. Not yet checked against Zimbabwean guidelines or registered products.'),
(8,'tomato','Bacterial spot','Bacterial disease. Small dark rough spots, often with a yellow halo, on leaves and fruit.','DRAFT from general knowledge. Not yet checked against Zimbabwean guidelines or registered products.');

INSERT INTO condition_symptoms (condition_id, symptom) VALUES
(1,'ORANGE_PUSTULES'),(1,'BROWN_SPOTS'),(1,'YELLOW_LEAVES'),
(2,'RECT_GRAY_LESIONS'),(2,'BROWN_SPOTS'),(2,'YELLOW_LEAVES'),
(3,'LONG_CIGAR_LESIONS'),(3,'BROWN_SPOTS'),(3,'YELLOW_LEAVES'),
(4,'HOLES_IN_LEAVES'),(4,'FRASS_IN_WHORL'),
(5,'YELLOW_STREAKS'),(5,'STUNTED'),(5,'YELLOW_LEAVES'),
(6,'TARGET_RINGS'),(6,'BROWN_SPOTS'),(6,'YELLOW_LEAVES'),
(7,'WATER_SOAKED'),(7,'BROWN_SPOTS'),(7,'FRUIT_SPOTS'),
(8,'SMALL_DARK_SPOTS'),(8,'FRUIT_SPOTS'),(8,'YELLOW_LEAVES');

INSERT INTO condition_treatments (condition_id, active_ingredient, category, advice) VALUES
(1,'propiconazole','FUNGICIDE','Triazole fungicide. Check the label for use on maize and for the safe interval before harvest.'),
(1,'azoxystrobin','FUNGICIDE','Strobilurin fungicide. Check the label for use on maize and for the safe interval before harvest.'),
(1,NULL,'NONE','Rotate crops, bury or remove infected crop residue, and plant tolerant varieties where available.'),
(2,'propiconazole','FUNGICIDE','Triazole fungicide. Check the label for use on maize and for the safe interval before harvest.'),
(2,'azoxystrobin','FUNGICIDE','Strobilurin fungicide. Check the label for use on maize and for the safe interval before harvest.'),
(2,NULL,'NONE','Rotate crops, bury or remove infected crop residue, and plant tolerant varieties where available.'),
(3,'propiconazole','FUNGICIDE','Triazole fungicide. Check the label for use on maize and for the safe interval before harvest.'),
(3,'azoxystrobin','FUNGICIDE','Strobilurin fungicide. Check the label for use on maize and for the safe interval before harvest.'),
(3,NULL,'NONE','Rotate crops, bury or remove infected crop residue, and plant tolerant varieties where available.'),
(4,'emamectin benzoate','INSECTICIDE','Check the label for use on maize, the application method and the safe interval before harvest.'),
(4,'chlorantraniliprole','INSECTICIDE','Check the label for use on maize, the application method and the safe interval before harvest.'),
(4,'spinetoram','INSECTICIDE','Check the label for use on maize, the application method and the safe interval before harvest.'),
(4,NULL,'NONE','Scout fields early and often. Treating small caterpillars early works best.'),
(5,NULL,'NONE','There is no cure. Remove and destroy infected plants early, control weeds and leafhoppers, and plant streak-tolerant varieties next season.'),
(6,'mancozeb','FUNGICIDE','Protectant fungicide. Check the label for use on tomato and for the safe interval before harvest.'),
(6,'chlorothalonil','FUNGICIDE','Protectant fungicide. Check the label for use on tomato and for the safe interval before harvest.'),
(6,NULL,'NONE','Remove badly infected lower leaves, avoid wetting the leaves when irrigating, and rotate away from tomato and potato.'),
(7,'mancozeb','FUNGICIDE','Protectant fungicide. Check the label for use on tomato and for the safe interval before harvest.'),
(7,'metalaxyl','FUNGICIDE','Often sold mixed with mancozeb. Check the label for use on tomato and for the safe interval before harvest.'),
(7,NULL,'NONE','Act quickly, it spreads fast in cool, wet weather. Avoid overhead irrigation and remove badly infected plants.'),
(8,'copper','BACTERICIDE','Copper-based product (for example copper oxychloride). Gives only partial control. Check the label.'),
(8,NULL,'NONE','Use clean seed or seedlings, avoid working in wet crops, and rotate crops.');
