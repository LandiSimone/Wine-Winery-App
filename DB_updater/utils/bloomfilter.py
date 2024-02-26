import json
import hashlib
import mmh3
from bitarray import bitarray
from pymongo import MongoClient


class BloomFilter:
    def __init__(self, size, hash_functions):
        self.size = size
        self.bit_array = bitarray(size)
        self.bit_array.setall(0)
        self.hash_functions = hash_functions

    def add(self, value):
        for func in self.hash_functions:
            index = func(value) % self.size
            self.bit_array[index] = 1

    def __contains__(self, value):
        for func in self.hash_functions:
            index = func(value) % self.size
            if not self.bit_array[index]:
                return False
        return True

# Funzioni hash


def hash1(value):
    return int(hashlib.sha256(value.encode()).hexdigest(), 16)


def hash2(value):
    return mmh3.hash(value)