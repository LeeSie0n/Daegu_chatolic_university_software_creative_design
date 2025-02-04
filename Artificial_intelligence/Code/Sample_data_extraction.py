# -*- coding: utf-8 -*-
"""Sample_Data_Extraction.ipynb

Automatically generated by Colab.

Original file is located at
    https://colab.research.google.com/drive/1EZo1BvQfyVHhFqKMQTaCizjN9L9w5ISi
"""

# Mount Google Drive and install necessary libraries

from google.colab import drive
drive.mount('/content/drive')

# Import necessary libraries

import pandas

# Load the dataset from the provided CSV file

file_path = '/content/drive/MyDrive/DaeguCatholicUniversity_Software_Creative_Design_01/Final_DataSet_LeeSioen_2024.11.27(AM_10_06)_(862,465).csv'

# Try reading with 'ISO-8859-1' or 'EUC-KR' encoding

data = pandas.read_csv(file_path, encoding='ISO-8859-1')  # Use ISO-8859-1 encoding

# Print the total number of rows in the dataset

print("Total number of rows in the dataset:", data.shape[0])

# Randomly sample 100,000 rows for a balanced subset of data

sampled_data = data.sample(n=100000, random_state=42)

# Save the sampled data to a new CSV file

output_path = '/content/drive/MyDrive/DaeguCatholicUniversity_Software_Creative_Design_01/Sample_Data_Extraction_(100,001).csv'
sampled_data.to_csv(output_path, index=False)

# Confirm the location of the saved file

print(f"Sampled data has been saved to {output_path}")