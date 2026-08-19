from setuptools import setup, find_packages
setup(
    name="velice",
    version="0.1.0",
    description="Velice programming language interpreter, toolchain, and GUI runtime",
    author="samwise1776",
    packages=find_packages(),
    entry_points={"console_scripts": ["velice=velice.cli:main"]},
    python_requires=">=3.10",
    package_data={"velice": ["*.py"]},
)
